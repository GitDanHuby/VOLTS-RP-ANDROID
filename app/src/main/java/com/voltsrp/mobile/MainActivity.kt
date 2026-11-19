
package com.voltsrp.mobile

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.voltsrp.mobile.ui.theme.MyComposeApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.KeyboardArrowUp

private val Purple = Color(0xFF7800FF)

class MainActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    private fun ativarTelaCheia() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ativarTelaCheia()

        setContent {
            MyComposeApplicationTheme {
                VoltsApp()
            }
        }
    }
}

@Composable
fun VoltsApp() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var user by remember { mutableStateOf(auth.currentUser) }
    var loading by remember { mutableStateOf(false) }
    var signingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun signInGoogle() {
        if (signingIn) return

        scope.launch {
            signingIn = true
            loginError = ""

            try {
                val credentialManager = CredentialManager.create(context)

                val googleOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(
        context.getString(R.string.default_web_client_id)
    )
    .setAutoSelectEnabled(false)
    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { task ->
                            signingIn = false

                            if (task.isSuccessful) {
                                user = auth.currentUser
                                loginError = ""
                            } else {
                                loginError = task.exception?.localizedMessage
                                    ?: "Não foi possível conectar sua conta."
                            }
                        }
                } else {
                    signingIn = false
                    loginError = "Tipo de conta Google não reconhecido."
                }

                        } catch (e: GetCredentialException) {
    android.util.Log.e(
        "VOLTS_AUTH",
        "TIPO: ${e.type} | CLASSE: ${e.javaClass.name}",
        e
    )

    signingIn = false
    loginError = "${e.javaClass.simpleName}: ${e.type}: ${e.message}"

} catch (e: Exception) {
    signingIn = false
    loginError = e.localizedMessage
        ?: "Erro ao conectar com Google."
}
        }
    }

    fun switchAccount() {
        auth.signOut()
        user = null
        loading = false
        loginError = ""

        scope.launch {
            try {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // A sessão Firebase já foi encerrada.
            }
        }
    }

    BackHandler(enabled = loading) {
        loading = false
    }

    if (user == null) {
        GoogleLoginScreen(
            signingIn = signingIn,
            error = loginError,
            onLogin = { signInGoogle() }
        )
    } else if (loading) {
        LoadingScreen(onBack = { loading = false })
    } else {
        VoltsHome(
            userName = user?.displayName ?: "Conta Google",
            userEmail = user?.email ?: "",
            photoUrl = user?.photoUrl?.toString(),
            onSwitchAccount = { switchAccount() },
            onPlay = { loading = true }
        )
    }
}




@Composable
fun GoogleLoginScreen(
    signingIn: Boolean,
    error: String,
    onLogin: () -> Unit
) {
    var acceptedTerms by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("PORTUGUÊS BRASIL") }

    val uriHandler = LocalUriHandler.current

    val languages = listOf(
        "🇧🇷" to "PORTUGUÊS BRASIL",
        "🇺🇸" to "ENGLISH",
        "🇪🇸" to "ESPAÑOL"
    )

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.volts_city_background),
            contentDescription = "VOLTS RP",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                .padding(start = 42.dp, end = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "VOLTS RP",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "CONECTE-SE E ENTRE NA CIDADE",
                color = Color(0xFFD5B8FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onLogin,
                enabled = !signingIn && acceptedTerms,
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF202124),
                    disabledContainerColor = Color.White.copy(alpha = 0.75f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 5.dp,
                    pressedElevation = 1.dp
                )
            ) {
                if (signingIn) {
                    CircularProgressIndicator(
                        color = Color(0xFF4285F4),
                        modifier = Modifier.size(21.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(12.dp))
                    Text("CONECTANDO...")
                } else {
                    Text(
                        "G",
                        color = Color(0xFF4285F4),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.width(13.dp))

                    Text(
                        "Conecte-se com o Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (error.isNotBlank()) {
                Spacer(Modifier.height(14.dp))

                Text(
                    error,
                    color = Color(0xFFFF7777),
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 42.dp, bottom = 24.dp)
                .background(
                    Color.Black.copy(alpha = 0.48f),
                    RoundedCornerShape(13.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4285F4),
                    uncheckedColor = Color.White,
                    checkmarkColor = Color.White
                )
            )

            Spacer(Modifier.width(6.dp))

            Column {
                Text(
                    "Eu concordo com os",
                    color = Color.White,
                    fontSize = 11.sp
                )

                Text(
                    "Termos de Uso",
                    color = Color(0xFFD5B8FF),
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(
                            "https://docs.google.com/document/d/1VBHRIM5l56UgGtCOef1M3dGMZbrwecusuhXexYS110Q/edit"
                        )
                    }
                )

                Text(
                    "Política de Privacidade",
                    color = Color(0xFFD5B8FF),
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(
                            "https://docs.google.com/document/d/1bHAfuq-jNReoDB69T21EUSiMlT-t3v3K2YjPDERPGB4/edit"
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 42.dp, bottom = 24.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.48f),
                            RoundedCornerShape(30.dp)
                        )
                        .clickable {
                            languageMenu = true
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        languages.firstOrNull {
                            it.second == selectedLanguage
                        }?.first ?: "🇧🇷",
                        fontSize = 17.sp
                    )

                    Spacer(Modifier.width(9.dp))

                    Text(
                        selectedLanguage,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Selecionar idioma",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = languageMenu,
                    onDismissRequest = {
                        languageMenu = false
                    }
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text("${language.first}  ${language.second}")
                            },
                            onClick = {
                                selectedLanguage = language.second
                                languageMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}





@Composable
fun VoltsHome(
    userName: String,
    userEmail: String,
    photoUrl: String?,
    onSwitchAccount: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current

    val playersOnline = 0
    val maxPlayers = 100
    val occupancy = playersOnline.toFloat() / maxPlayers

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        val sidePadding = screenW * 0.025f
        val bottomPadding = screenH * 0.035f

        // FUNDO
        Image(
            painter = painterResource(R.drawable.volts_city_background),
            contentDescription = "Cidade VOLTS RP",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // SOMBRA
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55000000),
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0xCC000000)
                        )
                    )
                )
        )

        // PERFIL
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = sidePadding, top = screenH * 0.035f)
                .background(
                    Color(0xCC100D19),
                    RoundedCornerShape(30.dp)
                )
                .padding(5.dp)
        ) {
            AccountMenu(
                name = userName,
                email = userEmail,
                photoUrl = photoUrl,
                onSwitchAccount = onSwitchAccount
            )
        }

        
        // INDICADOR PEQUENO DE JOGADORES ONLINE
Row(
    modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = sidePadding, top = screenH * 0.035f)
        .background(
            Color(0xCC100D19),
            RoundedCornerShape(30.dp)
        )
        .border(
            1.dp,
            Color(0x557D70A0),
            RoundedCornerShape(30.dp)
        )
        .padding(horizontal = 13.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        Modifier
            .size(7.dp)
            .background(Color(0xFF38E58C), CircleShape)
    )

    Spacer(Modifier.width(7.dp))

    Text(
        "$playersOnline ONLINE",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}
        // BARRA ÚNICA DAS REDES SOCIAIS
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = sidePadding,
                    bottom = bottomPadding
                )
                .background(
                    Color(0xBB100D19),
                    RoundedCornerShape(14.dp)
                )
                .border(
                    1.dp,
                    Color(0x557D70A0),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_discord),
                contentDescription = "Discord",
                modifier = Modifier
                    .size(25.dp)
                    .clickable {
                        openLink(context, "https://discord.gg/489B2V9a9w")
                    }
            )

            Image(
                painter = painterResource(R.drawable.ic_instagram),
                contentDescription = "Instagram",
                modifier = Modifier
                    .size(25.dp)
                    .clickable {
                        openLink(
                            context,
                            "https://www.instagram.com/voltsrp?rpxt=aTg3MWpiZzdlZGNx"
                        )
                    }
            )

            Image(
                painter = painterResource(R.drawable.ic_youtube),
                contentDescription = "YouTube",
                modifier = Modifier
                    .size(25.dp)
                    .clickable {
                        openLink(
                            context,
                            "https://youtube.com/@voltsrp?si=H_oZ46Hm-Vg4Bhur"
                        )
                    }
            )
        }

        // SELEÇÃO DE SERVIDOR
Card(
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(
            end = sidePadding,
            bottom = bottomPadding + screenH * 0.075f
        )
        .width(screenW * 0.36f)
        .height(screenH * 0.16f),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color(0xCC100D19)
    ),
    border = BorderStroke(1.dp, Color(0x557D70A0)),
    elevation = CardDefaults.cardElevation(5.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🇧🇷", fontSize = 17.sp)

                Spacer(Modifier.width(7.dp))

                Text(
                    "VOLTS ROLEPLAY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(7.dp))

            Text(
                "$playersOnline / $maxPlayers jogadores",
                color = Color(0xFFBDB7C8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.padding(start = 8.dp)
) {
    Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = "Servidor anterior",
        tint = Color.White,
        modifier = Modifier.size(19.dp)
    )

    Text(
        "TOQUE PARA\nSELECIONAR",
        color = Color.White,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Próximo servidor",
        tint = Color.White,
        modifier = Modifier.size(19.dp)
    )
}
    }
}
        // JOGAR AGORA
        Button(
            onClick = onPlay,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = sidePadding,
                    bottom = bottomPadding
                )
                .width(screenW * 0.30f)
                .height(screenH * 0.075f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1677F9)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 7.dp,
                pressedElevation = 2.dp
            )
        ) {
            Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "▶",
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black
    )
    Spacer(Modifier.width(5.dp))
    Text(
        "JOGAR AGORA",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        maxLines = 1,
        softWrap = false
    )
}
        }

        // RODAPÉ
        Text(
            "VOLTS RP © 2026",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}



@Composable
fun AccountMenu(
    name: String,
    email: String,
    photoUrl: String?,
    onSwitchAccount: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .background(
                    Color(0xCC17121F),
                    RoundedCornerShape(30.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Foto da conta Google",
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(34.dp).background(
                        Purple,
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(9.dp))

            Column {
                Text(
                    name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "Trocar de conta",
                    color = Color(0xFFD5B8FF),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.width(8.dp))
            Text("⌄", color = Color.White, fontSize = 22.sp)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Trocar de conta") },
                onClick = {
                    expanded = false
                    onSwitchAccount()
                }
            )
        }
    }
}

@Composable
fun LoadingScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val mediaPlayer: android.media.MediaPlayer? = remember(context) {
    android.media.MediaPlayer.create(context, R.raw.cara_do_crime)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(0.7f) }
    var progress by remember { mutableStateOf(0f) }

    DisposableEffect(mediaPlayer) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
        }
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(mediaPlayer, volume) {
        mediaPlayer?.setVolume(volume, volume)
    }

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(120)
            progress = (progress + 0.01f).coerceAtMost(1f)
        }
    }

    val status = when {
        progress < 0.20f -> "INICIALIZANDO VOLTS RP..."
        progress < 0.40f -> "PREPARANDO AMBIENTE..."
        progress < 0.60f -> "VERIFICANDO ARQUIVOS..."
        progress < 0.80f -> "PREPARANDO CONEXÃO..."
        progress < 1f -> "FINALIZANDO PREPARAÇÃO..."
        else -> "DEMONSTRAÇÃO CONCLUÍDA!"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        Image(
            painter = painterResource(R.drawable.volts_city_background),
            contentDescription = "Fundo VOLTS RP",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier.fillMaxSize().background(
                Color.Black.copy(alpha = 0.35f)
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                
                                "PREPARANDO SUA ENTRADA NA CIDADE",
                color = Color(0xFF8CCBFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = Color(0xFF168CFF),
                trackColor = Color.White.copy(alpha = 0.22f)
            )

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    status,
                    color = Color(0xFF8CCBFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.7f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("VOLTAR")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 14.dp)
                .width(280.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "A Cara do Crime",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "|◀",
                    color = Color.White,
                    fontSize = 19.sp,
                    modifier = Modifier.clickable {
                        mediaPlayer?.let {
                            it.seekTo(
                                (it.currentPosition - 10000).coerceAtLeast(0)
                            )
                        }
                    }.padding(horizontal = 5.dp)
                )

                Text(
                    if (isPlaying) "Ⅱ" else "▶",
                    color = Color.White,
                    fontSize = 21.sp,
                    modifier = Modifier.clickable {
                        mediaPlayer?.let {
                            if (it.isPlaying) {
                                it.pause()
                                isPlaying = false
                            } else {
                                it.start()
                                isPlaying = true
                            }
                        }
                    }.padding(horizontal = 5.dp)
                )

                Text(
                    "▶|",
                    color = Color.White,
                    fontSize = 19.sp,
                    modifier = Modifier.clickable {
                        mediaPlayer?.let {
                            it.seekTo(
                                (it.currentPosition + 10000)
                                    .coerceAtMost(it.duration)
                            )
                        }
                    }.padding(horizontal = 5.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    modifier = Modifier.width(85.dp).height(30.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
fun SocialButton(
    icon: Int,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.65f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = description,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Não foi possível abrir o link.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

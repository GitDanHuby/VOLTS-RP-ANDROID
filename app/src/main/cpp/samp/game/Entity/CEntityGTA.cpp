#include "CEntityGTA.h"
#include "game/game.h"
#include "game/Collision/ColStore.h"
#include "patch.h"
#include "game/General.h"
#include "game/Models/BaseModelInfo.h"
#include "game/Models/ModelInfo.h"
#include "game/References.h"
#include "game/Pools.h"

extern CGame* pGame;

CEntityGTA::CEntityGTA() : CPlaceable() {
    m_nStatus = STATUS_ABANDONED;
    m_nType = ENTITY_TYPE_NOTHING;

    m_nFlags = 0;
    m_bIsVisible = true;
    m_bBackfaceCulled = true;

    m_nScanCode = 0;
    m_nAreaCode = eAreaCodes::AREA_CODE_NORMAL_WORLD;
    m_nModelIndex = 0xFFFF;
    m_pRwObject = nullptr;
    m_nIplIndex = 0;
    m_nRandomSeed = CGeneral::GetRandomNumber();
    m_pReferences = nullptr;
    m_pStreamingLink = nullptr;
    m_nNumLodChildren = 0;
    m_nNumLodChildrenRendered = 0;
    m_pLod = nullptr;
}

CEntityGTA::~CEntityGTA()
{
    if (m_pLod)
        m_pLod->m_nNumLodChildren--;

    CEntityGTA::DeleteRwObject();
    CEntityGTA::ResolveReferences();
}

void CEntityGTA::ResolveReferences()
{
    auto refs = m_pReferences;
    while (refs) {
        if (*refs->m_ppEntity == this)
            *refs->m_ppEntity = nullptr;

        refs = refs->m_pNext;
    }

    refs = m_pReferences;
    if (!refs)
        return;

    refs->m_ppEntity = nullptr;
    while (refs->m_pNext)
        refs = refs->m_pNext;

    refs->m_pNext = CReferences::pEmptyList;
    CReferences::pEmptyList = m_pReferences;
    m_pReferences = nullptr;
}

void CEntityGTA::Add()
{
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x3ED8D8 + 1 : 0x4CD574), this);
}

void CEntityGTA::SetInterior(int interiorId, bool needRefresh)
{
    m_nAreaCode = static_cast<eAreaCodes>(interiorId);

    if ( this == (CEntityGTA*)pGame->FindPlayerPed()->m_pPed )
    {
        CGame::currArea = interiorId;

        auto pos = GetPosition();
        CColStore::RequestCollision(&pos, m_nAreaCode);

        if(interiorId == 0) {
            CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x00420898 + 1 : 0x504194), false);
        }

    }

}

float CEntityGTA::GetDistanceFromLocalPlayerPed() const
{
    auto pLocalPlayerPed = pGame->FindPlayerPed();

    return DistanceBetweenPoints(GetPosition(), pLocalPlayerPed->m_pPed->GetPosition());
}

float CEntityGTA::GetDistanceFromCamera()
{
    if(!this)
        return 0;

    CCamera& TheCamera = *reinterpret_cast<CCamera*>(g_libGTASA + (VER_x32 ? 0x00951FA8 : 0xBBA8D0));
    return DistanceBetweenPoints(GetPosition(), TheCamera.GetPosition());
}

float CEntityGTA::GetDistanceFromPoint(float X, float Y, float Z) const
{
    CVector vec(X, Y, Z);

    return DistanceBetweenPoints(GetPosition(), vec);
}

void CEntityGTA::SetCollisionChecking(bool bCheck)
{
    m_bCollisionProcessed = bCheck;
}

void CEntityGTA::UpdateRpHAnim() {
    if (const auto atomic = GetFirstAtomic(m_pRwClump)) {
        if (RpSkinGeometryGetSkin(RpAtomicGetGeometry(atomic)) && !m_bDontUpdateHierarchy) {
            RpHAnimHierarchyUpdateMatrices(GetAnimHierarchyFromSkinClump(m_pRwClump));
        }
    }
}

void CEntityGTA::UpdateRwFrame()
{
    if (!m_pRwObject)
        return;

    RwFrameUpdateObjects(static_cast<RwFrame*>(rwObjectGetParent(m_pRwObject)));
}

void CEntityGTA::DeleteRwObject()
{
    if(!*(uintptr*)this) return;

    (( void (*)(CEntityGTA*))(*(void**)(*(uintptr*)this + (VER_x32 ? 0x24 : 0x24*2))))(this);
}

void CEntityGTA::UpdateRW() {
    if (!this || !m_pRwObject) return;

    auto parentMatrix = GetModellingMatrix();
    if (!parentMatrix) return;

    if (m_matrix)
        m_matrix->UpdateRwMatrix(parentMatrix);
    else
        m_placement.UpdateRwMatrix(parentMatrix);
}

RwMatrix* CEntityGTA::GetModellingMatrix() {
    if (!m_pRwObject)
        return nullptr;

    return RwFrameGetMatrix(RwFrameGetParent(m_pRwObject));
}

CBaseModelInfo* CEntityGTA::GetModelInfo() const {
    return CModelInfo::GetModelInfo(m_nModelIndex);
}

void CEntityGTA::Add(const CRect* rect) {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x3ED8FC + 1 : 0x4CD5C0), this, rect);
}

void CEntityGTA::Remove() {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x3EDBE8 + 1 : 0x4CD888), this);
}

void CEntityGTA::SetModelIndex(uint32_t index) {
}

void CEntityGTA::SetModelIndexNoCreate(uint32_t index) {
}

void CEntityGTA::SetIsStatic(bool isStatic) {
}

namespace
{
    CColModel* GetEntityCollisionModel(const CEntityGTA* entity)
    {
        if (!entity || entity->m_nModelIndex == 0xFFFF ||
            entity->m_nModelIndex >= CModelInfo::NUM_MODEL_INFOS)
            return nullptr;

        CBaseModelInfo* modelInfo = CModelInfo::GetModelInfo(entity->m_nModelIndex);
        return modelInfo ? modelInfo->GetColModel() : nullptr;
    }

    CVector TransformEntityLocalPoint(const CEntityGTA* entity, const CVector& point)
    {
        const CVector position = entity->GetPosition();
        const CVector right = const_cast<CEntityGTA*>(entity)->GetRightVector();
        const CVector forward = const_cast<CEntityGTA*>(entity)->GetForwardVector();
        const CVector up = const_cast<CEntityGTA*>(entity)->GetUpVector();
        return position + right * point.x + forward * point.y + up * point.z;
    }
}

CRect CEntityGTA::GetBoundRect()
{
    const CColModel* colModel = GetEntityCollisionModel(this);
    if (!colModel)
    {
        const CVector pos = GetPosition();
        return CRect(pos.x, pos.y, pos.x, pos.y);
    }

    const CVector& minimum = colModel->m_boundBox.m_vecMin;
    const CVector& maximum = colModel->m_boundBox.m_vecMax;
    bool first = true;
    CRect rect{};

    for (int x = 0; x < 2; ++x)
    {
        for (int y = 0; y < 2; ++y)
        {
            for (int z = 0; z < 2; ++z)
            {
                const CVector local(
                    x ? maximum.x : minimum.x,
                    y ? maximum.y : minimum.y,
                    z ? maximum.z : minimum.z
                );
                const CVector world = TransformEntityLocalPoint(this, local);

                if (first)
                {
                    rect.left = rect.right = world.x;
                    rect.top = rect.bottom = world.y;
                    first = false;
                }
                else
                {
                    rect.left = std::min(rect.left, world.x);
                    rect.right = std::max(rect.right, world.x);
                    rect.top = std::max(rect.top, world.y);
                    rect.bottom = std::min(rect.bottom, world.y);
                }
            }
        }
    }

    return rect;
}

CVector CEntityGTA::GetBoundCentre()
{
    const CColModel* colModel = GetEntityCollisionModel(this);
    if (!colModel)
        return GetPosition();

    return TransformEntityLocalPoint(this, colModel->m_boundSphere.m_vecCenter);
}

float CEntityGTA::GetBoundRadius()
{
    const CColModel* colModel = GetEntityCollisionModel(this);
    if (!colModel)
        return 1.0f;

    const CVector right = GetRightVector();
    const CVector forward = GetForwardVector();
    const CVector up = GetUpVector();
    const auto magnitude = [](const CVector& v) {
        return std::sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    };
    const float scale = std::max({ magnitude(right), magnitude(forward), magnitude(up) });
    return colModel->m_boundSphere.m_fRadius * scale;
}

void CEntityGTA::RegisterReference(CEntityGTA** entity)
{
    if (IsBuilding() && !m_bIsTempBuilding && !m_bIsProcObject && !m_nIplIndex)
        return;

    auto refs = m_pReferences;
    while (refs) {
        if (refs->m_ppEntity == entity) {
            return;
        }
        refs = refs->m_pNext;
    }

    if (!m_pReferences && !CReferences::pEmptyList) {
        auto iPedsSize = GetPedPoolGta()->GetSize();
        for (int32 i = 0; i < iPedsSize; ++i) {
            auto ped = GetPedPoolGta()->GetAt(i);
            if (ped) {
                ped->PruneReferences();
                if (CReferences::pEmptyList)
                    break;
            }

        }

        if (!CReferences::pEmptyList) {
            auto iVehsSize = GetVehiclePoolGta()->GetSize();
            for (int32 i = 0; i < iVehsSize; ++i) {
                auto vehicle = GetVehiclePoolGta()->GetAt(i);
                if (vehicle) {
                    vehicle->PruneReferences();
                    if (CReferences::pEmptyList)
                        break;
                }

            }
        }

        if (!CReferences::pEmptyList) {
            auto iObjectsSize = GetObjectPoolGta()->GetSize();
            for (int32 i = 0; i < iObjectsSize; ++i) {
                auto obj = GetObjectPoolGta()->GetAt(i);
                if (obj) {
                    obj->PruneReferences();
                    if (CReferences::pEmptyList)
                        break;
                }
            }
        }
    }

    if (CReferences::pEmptyList) {
        auto pEmptyRef = CReferences::pEmptyList;
        CReferences::pEmptyList = pEmptyRef->m_pNext;
        pEmptyRef->m_pNext = m_pReferences;
        m_pReferences = pEmptyRef;
        pEmptyRef->m_ppEntity = entity;
    }
}

void CEntityGTA::PruneReferences()
{
    if (!m_pReferences)
        return;

    auto refs = m_pReferences;
    auto ppPrev = &m_pReferences;
    while (refs) {
        if (*refs->m_ppEntity == this) {
            ppPrev = &refs->m_pNext;
            refs = refs->m_pNext;
        }
        else {
            auto refTemp = refs->m_pNext;
            *ppPrev = refs->m_pNext;
            refs->m_pNext = CReferences::pEmptyList;
            CReferences::pEmptyList = refs;
            refs->m_ppEntity = nullptr;
            refs = refTemp;
        }
    }
}

void CEntityGTA::CleanUpOldReference(CEntityGTA** entity)
{
    if (!m_pReferences)
        return;

    auto refs = m_pReferences;
    auto ppPrev = &m_pReferences;
    while (refs->m_ppEntity != entity) {
        ppPrev = &refs->m_pNext;
        refs = refs->m_pNext;
        if (!refs)
            return;
    }

    *ppPrev = refs->m_pNext;
    refs->m_pNext = CReferences::pEmptyList;
    refs->m_ppEntity = nullptr;
    CReferences::pEmptyList = refs;
}
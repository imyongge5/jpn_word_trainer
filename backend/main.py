from fastapi import FastAPI

from database import Base, engine, migrate_legacy_sync_schema
from routers.auth_router import router as auth_router
from routers.builtin_router import router as builtin_router
from routers.health_router import router as health_router
from routers.sync_router import router as sync_router


migrate_legacy_sync_schema()
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Mistbottle JPN Word Trainer API")

app.include_router(health_router)
app.include_router(auth_router)
app.include_router(builtin_router)
app.include_router(sync_router)

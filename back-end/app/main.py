from contextlib import asynccontextmanager
from fastapi import FastAPI
from .database import init_db
from .api.v1.api import api_router
from fastapi.staticfiles import StaticFiles

# 定义生命周期
@asynccontextmanager
async def lifespan(app: FastAPI):
    print("正在初始化数据库...")
    init_db()
    print("数据库初始化完成！")
    yield
    print("服务器正在关闭...")

# 初始化 App
app = FastAPI(title="Delta3D", lifespan=lifespan)
app.include_router(api_router, prefix="/api/v1")

app.mount("/static", StaticFiles(directory="static"), name="static")

@app.get("/")
def root():
    return {"message": "Hello Delta3D!"}


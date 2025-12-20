from fastapi import APIRouter
from app.api.v1.endpoints import auth, posts, assets, stream, users, chat

# 主路由
api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["认证"])

api_router.include_router(posts.router, prefix="/posts", tags=["帖子/社区"])
api_router.include_router(assets.router, prefix="/assets", tags=["模型资产"])

api_router.include_router(stream.router, prefix="/stream", tags=["模型流/远程控制"])

api_router.include_router(users.router, prefix="/user", tags=['用户信息'])

api_router.include_router(chat.router, prefix="/chat", tags=["chat"])

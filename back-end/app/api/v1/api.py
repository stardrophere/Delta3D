from fastapi import APIRouter
from app.api.v1.endpoints import auth, posts, assets, stream

api_router = APIRouter()

# 把 auth.py 里的路由挂载进来，标签叫 "认证"
api_router.include_router(auth.router, prefix="/auth", tags=["认证"])

api_router.include_router(posts.router, tags=["帖子/社区"])
# 2. 注册 assets 路由
api_router.include_router(assets.router, prefix="/assets", tags=["模型资产"])

api_router.include_router(stream.router, prefix="/stream", tags=["模型流/远程控制"])

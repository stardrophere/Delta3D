# run.py
import uvicorn

if __name__ == "__main__":
    # 调用 uvicorn.run() 启动服务
    uvicorn.run(
        app="app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        workers=1
    )

# upload_asset_test.py
import os
import json
import requests

API_URL = "http://127.0.0.1:8000/api/v1/assets/upload"
VIDEO_PATH = r"E:\ScnuProject\Verification\c.mp4"

# 如果你的接口需要登录（Depends(get_current_user)），通常要带 token
# 下面两种方式二选一：
TOKEN = os.getenv("TOKEN", "")  # 你可以在环境变量里设置 TOKEN
# TOKEN = "YOUR_JWT_TOKEN"

def main():
    if not os.path.exists(VIDEO_PATH):
        raise FileNotFoundError(VIDEO_PATH)

    headers = {}

    headers["Authorization"] = f"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjY1MDkyODQsInN1YiI6IjQifQ.eJH_2ki1l8ssL0qErFLVhUbxCFU4Zh5Nod2JBogm6hM"

    data = {
        "title": "测试资产",
        "description": "这是一个上传测试",
        "tags": "猫,动物,3D",
        "remark": "hello",
    }

    # 重点：multipart 上传
    # field name 必须叫 "file"（和你后端 UploadFile = File(...) 对应）
    with open(VIDEO_PATH, "rb") as f:
        files = {
            "file": (os.path.basename(VIDEO_PATH), f, "video/mp4"),
        }
        resp = requests.post(API_URL, headers=headers, data=data, files=files, timeout=300)

    print("Status:", resp.status_code)
    print("Headers:", resp.headers.get("content-type"))

    # 尝试解析 JSON
    try:
        payload = resp.json()
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    except Exception:
        print(resp.text)

if __name__ == "__main__":
    main()

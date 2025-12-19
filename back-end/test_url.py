import os
import json
import requests

# 1. 对应你 users.py 路由中定义的接口路径
API_URL = "http://127.0.0.1:8000/api/v1/user/me/avatar"
# 2. 修改为你的本地图片路径
IMAGE_PATH = r"E:\ScnuProject\Verification\default.png"


def main():
    if not os.path.exists(IMAGE_PATH):
        print(f"错误: 找不到文件 {IMAGE_PATH}")
        return

    # 请确保 Token 是有效的
    headers = {
        "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjY3Mjk5ODgsInN1YiI6IjEifQ._R3DTSJ8kb_mKOqQXzhuFqvYXFNoZ7M4greX1UBSt5o"
    }

    # 注意：头像上传接口通常不需要 data 字段（除非你在后端 Form(...) 定义了其他字段）
    # 但为了保险，如果后端没定义，传空即可
    data = {}

    print(f"正在上传头像: {os.path.basename(IMAGE_PATH)} ...")

    with open(IMAGE_PATH, "rb") as f:
        # 这里的 key 必须叫 "file"，对应后端 file: UploadFile = File(...)
        files = {
            "file": (os.path.basename(IMAGE_PATH), f, "image/jpeg"),
        }

        try:
            # 头像一般较小，timeout 不需要设置像视频那么大
            resp = requests.post(API_URL, headers=headers, data=data, files=files, timeout=30)

            print("Status:", resp.status_code)

            # 尝试解析返回的 UserDetail 或 UserOut JSON
            try:
                payload = resp.json()
                print("返回结果:")
                print(json.dumps(payload, ensure_ascii=False, indent=2))

                if resp.status_code == 200:
                    print(f"\n成功！新头像访问路径: {payload.get('avatar_url')}")
            except Exception:
                print("非 JSON 响应内容:")
                print(resp.text)

        except requests.exceptions.RequestException as e:
            print(f"请求发生异常: {e}")


if __name__ == "__main__":
    main()

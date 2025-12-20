import os
import json
import requests


API_URL = "http://127.0.0.1:8000/api/v1/user/me/avatar"
IMAGE_PATH = r"E:\ScnuProject\Verification\default.png"


def main():
    if not os.path.exists(IMAGE_PATH):
        print(f"错误: 找不到文件 {IMAGE_PATH}")
        return

    # Token
    headers = {
        "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjY3Mjk5ODgsInN1YiI6IjEifQ._R3DTSJ8kb_mKOqQXzhuFqvYXFNoZ7M4greX1UBSt5o"
    }


    data = {}

    print(f"正在上传头像: {os.path.basename(IMAGE_PATH)} ...")

    with open(IMAGE_PATH, "rb") as f:

        files = {
            "file": (os.path.basename(IMAGE_PATH), f, "image/jpeg"),
        }

        try:

            resp = requests.post(API_URL, headers=headers, data=data, files=files, timeout=30)

            print("Status:", resp.status_code)


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

from sqlmodel import Session
from app.database import engine  # 导入 engine 用于在后台任务中创建新的 Session
from app.models import AssetStatus, ModelAsset


# 假设你的训练函数放在 app.core.train_utils 或类似位置
from app.ngp.creater import train_ngp_from_video

def task_train_asset(asset_id: int, video_disk_path: str, snapshot_disk_path: str, web_model_path: str):
    """
    后台任务：执行训练并更新数据库状态
    """
    # 所以我们需要创建一个新的 Session
    with Session(engine) as session:
        # 1. 获取资产对象
        asset = session.get(ModelAsset, asset_id)
        if not asset:
            return

        # 2. 更新状态 -> PROCESSING
        asset.status = AssetStatus.PROCESSING
        session.add(asset)
        session.commit()

        try:
            print(f"开始训练 Asset ID: {asset_id}...")

            # 3.训练函数
            train_result = train_ngp_from_video(
                video_path=video_disk_path,
                snapshot_path=snapshot_disk_path,

                n_steps=5000
            )

            # 4. 训练成功
            print(f"训练完成: {train_result}")
            asset.status = AssetStatus.COMPLETED
            asset.model_path = web_model_path

        except Exception as e:
            # 5. 训练失败：更新状态
            print(f"训练失败 Asset ID {asset_id}: {e}")
            asset.status = AssetStatus.FAILED
            # asset.remark = f"{asset.remark or ''} | Error: {str(e)}"

        finally:
            # 最终提交数据库修改
            session.add(asset)
            session.commit()
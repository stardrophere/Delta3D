from sqlmodel import Session
from app.database import engine
from app.models import AssetStatus, ModelAsset
from app.ngp.creater import train_ngp_from_video

def task_train_asset(asset_id: int, video_disk_path: str, snapshot_disk_path: str, web_model_path: str):
    """
    后台任务：执行训练并更新数据库状态
    """

    with Session(engine) as session:
        # 获取model对象
        asset = session.get(ModelAsset, asset_id)
        if not asset:
            return

        #更新状态 -> PROCESSING
        asset.status = AssetStatus.PROCESSING
        session.add(asset)
        session.commit()

        try:
            print(f"开始训练 Asset ID: {asset_id}...")

            # 训练函数
            train_result = train_ngp_from_video(
                video_path=video_disk_path,
                snapshot_path=snapshot_disk_path,

                n_steps=5000
            )

            # 训练成功
            print(f"训练完成: {train_result}")
            asset.status = AssetStatus.COMPLETED
            asset.model_path = web_model_path

        except Exception as e:
            # 训练失败：更新状态
            print(f"训练失败 Asset ID {asset_id}: {e}")
            asset.status = AssetStatus.FAILED
            # asset.remark = f"{asset.remark or ''} | Error: {str(e)}"

        finally:
            # 提交数据库修改
            session.add(asset)
            session.commit()
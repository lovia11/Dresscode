from __future__ import annotations

from sqlalchemy import BigInteger, Boolean, Column, Index, Integer, String, Text
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class ClosetItem(Base):
    __tablename__ = "closet_items"

    id = Column(Integer, primary_key=True, autoincrement=True)
    owner = Column(String(64), nullable=False, index=True)
    name = Column(String(128), nullable=False)
    category = Column(String(64), nullable=False)
    color = Column(String(64), nullable=False, default="")
    season = Column(String(64), nullable=False, default="")
    style = Column(String(64), nullable=False, default="")
    scene = Column(String(64), nullable=False, default="")
    is_favorite = Column(Boolean, nullable=False, default=False)

    image_name = Column(String(128), nullable=False)  # 对应 /files/{name}
    image_url = Column(Text, nullable=False, default="")  # 冗余存一份公网 URL，便于排查

    tags_json = Column(Text, nullable=False, default="")  # qwen-vl-plus 输出的结构化 JSON（字符串）
    tag_model = Column(String(64), nullable=False, default="")
    tag_updated_at = Column(BigInteger, nullable=False, default=0)

    created_at = Column(BigInteger, nullable=False, default=0)
    updated_at = Column(BigInteger, nullable=False, default=0)


Index("idx_closet_owner_created", ClosetItem.owner, ClosetItem.created_at)


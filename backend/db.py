from __future__ import annotations

import os
import time
from contextlib import contextmanager
from typing import Iterator

from sqlalchemy import create_engine
from sqlalchemy.engine.url import make_url
from sqlalchemy.orm import Session, sessionmaker


def now_ms() -> int:
    return int(time.time() * 1000)


def _database_url() -> str:
    # 默认用 sqlite 便于本地开发；部署到公网时换成 MySQL（见 README）。
    return (os.getenv("DATABASE_URL") or "sqlite:///backend_data/dresscode.db").strip()


DATABASE_URL = _database_url()

_url = make_url(DATABASE_URL)
_connect_args = {"check_same_thread": False} if _url.drivername.startswith("sqlite") else {}

engine = create_engine(
    DATABASE_URL,
    future=True,
    pool_pre_ping=True,
    connect_args=_connect_args,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


@contextmanager
def session_scope() -> Iterator[Session]:
    session = SessionLocal()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


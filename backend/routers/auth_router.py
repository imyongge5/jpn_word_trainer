import time

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from auth import create_access_token, hash_password, verify_password
from database import get_db
from models import User
from schemas import LoginRequest, RegisterRequest, TokenResponse


router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", status_code=status.HTTP_201_CREATED)
def register(request: RegisterRequest, db: Session = Depends(get_db)):
    username = request.username.strip()
    password = request.password.strip()
    if not username or not password:
        raise HTTPException(status_code=400, detail="아이디와 비밀번호를 입력해 주세요.")
    existing = db.query(User).filter(User.username == username).first()
    if existing:
        raise HTTPException(status_code=409, detail="이미 사용 중인 아이디입니다.")
    user = User(
        username=username,
        hashed_password=hash_password(password),
        created_at=int(time.time() * 1000),
    )
    db.add(user)
    db.commit()
    return {"created": True}


@router.post("/login", response_model=TokenResponse)
def login(request: LoginRequest, db: Session = Depends(get_db)):
    username = request.username.strip()
    password = request.password.strip()
    user = db.query(User).filter(User.username == username).first()
    if user is None or not verify_password(password, user.hashed_password):
        raise HTTPException(status_code=401, detail="아이디 또는 비밀번호가 올바르지 않습니다.")
    return TokenResponse(
        access_token=create_access_token(user),
        username=user.username,
    )

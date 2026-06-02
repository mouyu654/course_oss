"""Pydantic schemas — Teacher module (M3-1 / M3-2 / M3-3)"""

from __future__ import annotations

from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field, model_validator


# ── Shared ──────────────────────────────────────────────

class ObjectiveOut(BaseModel):
    id: str
    course_id: str
    code: str
    description: str
    dimension: str

    model_config = {"from_attributes": True}


class WeightEntry(BaseModel):
    objective_id: str
    indicator_id: str
    weight_w: Decimal = Field(..., ge=0, le=1, max_digits=5, decimal_places=4)


class AssessmentItemOut(BaseModel):
    id: str
    course_id: str
    objective_id: str
    name: str
    max_score: Decimal

    model_config = {"from_attributes": True}


# ── M3-1 课程目标 ───────────────────────────────────────

class ObjectiveCreate(BaseModel):
    course_id: str = Field(..., min_length=1)
    code: str = Field(..., min_length=1, max_length=10)
    description: str = Field(..., min_length=1)
    dimension: str = Field(..., min_length=1)

    @model_validator(mode="after")
    def check_dimension(self) -> "ObjectiveCreate":
        allowed = {"知识维度", "能力维度", "价值维度"}
        if self.dimension not in allowed:
            raise ValueError(f"dimension 必须是 {allowed} 之一，收到：{self.dimension}")
        return self


class ObjectiveListOut(BaseModel):
    objectives: List[ObjectiveOut]


# ── M3-2 内部权重矩阵 ───────────────────────────────────

class InternalWeightSubmit(BaseModel):
    """请求体：一组权重条目。按 indicator_id 分组校验 Σw = 1.0。"""
    weights: List[WeightEntry]

    @model_validator(mode="after")
    def check_indicator_sums(self) -> "InternalWeightSubmit":
        from collections import defaultdict
        sums: dict[str, Decimal] = defaultdict(lambda: Decimal("0"))
        for w in self.weights:
            sums[w.indicator_id] += w.weight_w
        TOL = Decimal("0.001")
        for ind_id, total in sums.items():
            if abs(total - Decimal("1")) > TOL:
                raise ValueError(
                    f"指标点 {ind_id} 的权重合计为 {total}，不等于 1.0（容差 ±0.001）"
                )
        return self


class WeightEntryOut(BaseModel):
    id: str
    objective_id: str
    indicator_id: str
    weight_w: Decimal

    model_config = {"from_attributes": True}


class InternalWeightListOut(BaseModel):
    weights: List[WeightEntryOut]


# ── M3-3 考核点 ─────────────────────────────────────────

class AssessmentItemCreate(BaseModel):
    course_id: str = Field(..., min_length=1)
    objective_id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1, max_length=200)
    max_score: Decimal = Field(..., gt=0, max_digits=6, decimal_places=2)


class AssessmentItemListOut(BaseModel):
    items: List[AssessmentItemOut]

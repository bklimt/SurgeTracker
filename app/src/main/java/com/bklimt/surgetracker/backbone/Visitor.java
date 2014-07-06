package com.bklimt.surgetracker.backbone;

public interface Visitor<T extends Model> {
    void visit(T model) throws Exception;
}

package com.bklimt.babywatch.backbone;

public interface CollectionListener<T1 extends Collection<T2>, T2 extends Model> {
    void onAdd(T1 collection, T2 item, int position);
    void onRemove(T1 collection, T2 item);
}

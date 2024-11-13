package dev.fileformat.drako;


/**
 * Created by lexchou on 3/3/2017.
 */
@Internal
class Tuple_2<T1, T2> {
    private T1 item1;
    private T2 item2;

    public Tuple_2()
    {

    }
    public Tuple_2(T1 t1, T2 t2)
    {
        this.item1 = t1;
        this.item2 = t2;
    }

    public T1 getItem1()
    {
        return item1;
    }
    public T2 getItem2()
    {
        return item2;
    }

    public void setItem1(T1 val)
    {
        item1 = val;
    }
    public void setItem2(T2 val)
    {
        item2 = val;
    }
}

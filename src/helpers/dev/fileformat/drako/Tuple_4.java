package dev.fileformat.drako;


/**
 * Created by lexchou on 3/3/2017.
 */
@Internal
class Tuple_4<T1, T2, T3, T4> {
    private T1 item1;
    private T2 item2;
    private T3 item3;
    private T4 item4;

    public Tuple_4()
    {

    }
    public Tuple_4(T1 t1, T2 t2, T3 t3, T4 t4)
    {
        this.item1 = t1;
        this.item2 = t2;
        this.item3 = t3;
        this.item4 = t4;
    }

    public T1 getItem1()
    {
        return item1;
    }
    public T2 getItem2()
    {
        return item2;
    }
    public T3 getItem3()
    {
        return item3;
    }
    public T4 getItem4()
    {
        return item4;
    }

    public void setItem1(T1 val)
    {
        item1 = val;
    }
    public void setItem2(T2 val)
    {
        item2 = val;
    }
    public void setItem3(T3 val)
    {
        item3 = val;
    }
    public void setItem4(T4 val)
    {
        item4 = val;
    }
}

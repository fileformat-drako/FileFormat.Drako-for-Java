package dev.fileformat.drako;


/**
 * Created by lexchou on 3/3/2017.
 */
@Internal
class Tuple_1<T1> {
    private T1 item1;

    public Tuple_1()
    {

    }
    public Tuple_1(T1 t1)
    {
        this.item1 = t1;
    }

    public T1 getItem1()
    {
        return item1;
    }

    public void setItem1(T1 val)
    {
        item1 = val;
    }
}

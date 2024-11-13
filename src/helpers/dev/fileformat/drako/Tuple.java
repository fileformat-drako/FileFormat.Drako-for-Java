package dev.fileformat.drako;


/**
 * Created by lexchou on 3/3/2017.
 */
@Internal
class Tuple {

    public static <T1> Tuple_1 create(T1 t1)
    {
        return new Tuple_1<T1>(t1);
    }
    public static <T1, T2> Tuple_2 create(T1 t1, T2 t2)
    {
        return new Tuple_2<T1, T2>(t1, t2);
    }
    public static <T1, T2, T3> Tuple_3 create(T1 t1, T2 t2, T3 t3)
    {
        return new Tuple_3<T1, T2, T3>(t1, t2, t3);
    }
    public static <T1, T2, T3, T4> Tuple_4 create(T1 t1, T2 t2, T3 t3, T4 t4)
    {
        return new Tuple_4<T1, T2, T3, T4>(t1, t2, t3, t4);
    }
}

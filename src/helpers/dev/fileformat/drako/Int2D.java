package dev.fileformat.drako;


/**
 * Created by lexchou on 5/17/2017.
 * 2-dimensional int array wrapper
 */
public final class Int2D {
    private int rows;
    private int columns;
    private int[] data;

    /**
     * Construct a 2D int array with default data allocation.
     * @param rows Number of rows of the 2D array
     * @param columns Number of columns of the 2D array
     */
    public Int2D(int rows, int columns)
    {
        this.rows = rows;
        this.columns = columns;
        this.data = new int[rows * columns];
    }

    /**
     * Construct a 2D int array with given data
     * @param rows Number of rows of the 2D array
     * @param columns Number of columns of the 2D array
     * @param data Array to wrap, Float2D will use this array internally.
     */
    public Int2D(int rows, int columns, int[] data)
    {
        this.rows = rows;
        this.columns = columns;
        this.data = data;
        if(data != null && data.length != rows * columns)
            throw new IllegalArgumentException("Unmatched array size");
        if(data == null)
            this.data = new int[rows * columns];
    }

    /**
     * Gets the element at specified position
     * @param r Row
     * @param c Column
     * @return the value at specified position
     */
    public int get(int r, int c)
    {
        return data[r * columns + c];
    }

    /**
     * Sets the element at specified position
     * @param r Row
     * @param c Column
     * @param v Value
     */
    public void set(int r, int c, int v)
    {
        data[r * columns + c] = v;
    }

    /**
     * Gets the total length of this 2d array
     * @return the total number of floats in this 2d array.
     */
    public int length()
    {
        return data.length;
    }

    public int getRows() {
        return rows;
    }
    public int getColumns() {
        return columns;
    }
    public int getLength(int rank) {
        switch(rank) {
            case 0:
                return rows;
            case 1:
                return columns;
            default:
                throw new IndexOutOfBoundsException();
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("int[");
        sb.append(rows);
        sb.append(", ");
        sb.append(columns);
        sb.append("]");
        return sb.toString();
    }
}

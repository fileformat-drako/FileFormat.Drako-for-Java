package org.openize.drako;
import com.aspose.csporter.helpers.Algorithms;
/**
 *  Used to simulate std::vector&lt;int&gt;
 *  here we don't use List&lt;int&gt; because we need to simulate some behavior from C++ version.
 *
 */
public class IntList
{    
    private int count;
    int[] data;
    public int getCount()
    {
        return count;
    }
    
    public IntList()
    {
        this.data = new int[10];
    }
    
    public IntList(int size)
    {
        this.data = new int[size];
        this.count = size;
    }
    
    public int getCapacity()
    {
        return data.length;
    }
    
    public void setCapacity(int value)
    {
        if (value <= data.length)
            return;
        int[] newData = new int[value];
        System.arraycopy(data, 0, newData, 0, data.length);
        this.data = newData;
    }
    
    public void add(int v)
    {
        this.ensureCapacity(count + 1);
        data[count++] = v;
    }
    
    public void addRange(int[] v)
    {
        this.addRange(v, v.length);
    }
    
    public void addRange(IntList v)
    {
        this.addRange(v.data, v.count);
    }
    
    public void addRange(int[] v, int length)
    {
        if (length > 0)
        {
            this.ensureCapacity(count + length);
            System.arraycopy(v, 0, data, count, length);
            count += length;
        }
        
    }
    
    public void clear()
    {
        this.count = 0;
    }
    
    public void removeAt(int idx)
    {
        if (idx == (count - 1))
        {
            count--;
        }
        else
        {
            System.arraycopy(data, idx + 1, data, idx, count - idx - 1);
            count--;
        }
        
    }
    
    public int get(int idx)
    {
        return data[idx];
    }
    
    public void set(int idx, int value)
    {
        data[idx] = value;
    }
    
    public int getBack()
    {
        return data[count - 1];
    }
    
    public void popBack()
    {
        if (count > 0)
        {
            count--;
        }
        
    }
    
    public void reverse()
    {
        Algorithms.reverse(data, 0, count);
    }
    
    public void resize(int newSize)
    {
        this.ensureCapacity(newSize);
        this.count = newSize;
    }
    
    public void resize(int newSize, int newValue)
    {
        this.ensureCapacity(newSize);
        for (int i = count; i < newSize; i++)
        {
            data[i] = newValue;
        }
        
        this.count = newSize;
    }
    
    private void ensureCapacity(int newSize)
    {
        if (newSize < data.length)
            return;
        int capacity = data.length;
        while (capacity < newSize)
        {
            capacity += capacity >> 1;
        }
        
        this.setCapacity(capacity);
    }
    
    public int[] toArray()
    {
        int[] ret = new int[count];
        System.arraycopy(data, 0, ret, 0, count);
        return ret;
    }
    
}

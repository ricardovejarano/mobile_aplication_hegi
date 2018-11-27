package opencv.hegi.countpeopleopencv;

class PersonCoordenade {
    private int horizontal;
    private int vertical;

    public PersonCoordenade()
    {
        horizontal = 0;
        vertical = 0;
    }
    public PersonCoordenade(int x , int y)
    {
        horizontal = x;
        vertical = y;
    }


    public int getHorizontal()
    {
        return horizontal;
    }

    //Método para establecer la edad del animal
    public void setHorizontal(int setHorizontal)
    {
        horizontal = setHorizontal;
    }

    //Método para obtener el nombre del animal
    public int getVertical()
    {
        return vertical;
    }

    public void setVertical(int setVertical)
    {
        vertical = setVertical;
    }
}

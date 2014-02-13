package com.bitalino.bitalinodroid; /**
 * Created by andrewmb8 on 2/10/14.
 Makes a tag which contains a data, frame, and position of the frame (order taken)


 */


public class Tag implements Comparable{

    public String date;
    public double reading;
    public int pos;

    public Tag(String date, double reading, int pos){
        this.date = date;
        this.reading= reading;
        this.pos = pos;
    }


    @Override
    public int compareTo(Object other) {
        Tag tag = (Tag)other;
        if(this.pos>tag.pos)return 1;
        else if(this.pos<tag.pos)return-1;
        else return 0;

    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Tag [Number=" + pos + "     date=" + date + ", reading=" + reading + "]";
    }


//    /* (non-Javadoc)
//     * @see java.lang.Object#hashCode()
//     */
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((date == null) ? 0 : date.hashCode());
//        result = prime * result + reading;
//        result = prime * result + pos;
//        return result;
//    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (reading != other.reading)
            return false;
        if (pos != other.pos)
            return false;
        return true;
    }
}
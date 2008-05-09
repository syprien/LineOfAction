package moteur;

public class Couple {
	private int i;
	private int j;
	
	public Couple(int i,int j){
		this.i=i;
		this.j=j;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Couple) {
			Couple val = (Couple)obj;
			if(val.i == this.i && val.j == this.j){
				return true;
			}
		}
		return false;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}
	
	public String toString() {
		String i = "";
        switch (this.i) {
        case 0:
            i = "A";
            break;
        case 1:
            i = "B";
            break;
        case 2:
            i = "C";
            break;
        case 3:
            i = "D";
            break;
        case 4:
            i = "E";
            break;
        case 5:
            i = "F";
            break;
        case 6:
            i = "G";
            break;
        case 7:
            i = "H";
            break;
        default :
        	i = "Z";
        }
		return i+(this.getJ()+1);
	}
	
}

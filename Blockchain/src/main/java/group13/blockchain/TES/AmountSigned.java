package group13.blockchain.TES;

public class AmountSigned{

    float value;
    byte[] signature;

    public AmountSigned(float v, byte[] sig){
        value = v;
        signature = sig;
    }

    public float getAmount(){ return value; }
    public byte[] getSignature() { return signature; }
    
}

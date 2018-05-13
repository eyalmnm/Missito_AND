package ch.mitto.missito.util;

public class CRC8
{
    private static final int CRC_POLYNOM = 0x9C;
    private static final byte CRC_INITIAL = (byte)0xFF;

    private final short init;
    private final short[]   crcTable = new short[256];
    private short   value;

    public CRC8() {
        this(CRC_POLYNOM, CRC_INITIAL);
    }

    public CRC8(short init) {
        this(CRC_POLYNOM, init);
    }

    /**
     * Construct a CRC8 specifying the polynomial and initial value.
     * @param polynomial Polynomial, typically one of the POLYNOMIAL_* constants.
     * @param init Initial value, typically either 0xff or zero.
     */
    public CRC8(int polynomial, short init)
    {
        this.value = this.init = init;
        for (int dividend = 0; dividend < 256; dividend++)
        {
            int remainder = dividend ;//<< 8;
            for (int bit = 0; bit < 8; ++bit)
                if ((remainder & 0x01) != 0)
                    remainder = (remainder >>> 1) ^ polynomial;
                else
                    remainder >>>= 1;
            crcTable[dividend] = (short)remainder;
        }
    }

    public void update(byte[] buffer, int offset, int len)
    {
        for (int i = 0; i < len; i++)
        {
            int data = buffer[offset+i] ^ value;
            value = (short)(crcTable[data & 0xff] ^ (value << 8));
        }
    }

    /**
     * Updates the current checksum with the specified array of bytes.
     * Equivalent to calling <code>update(buffer, 0, buffer.length)</code>.
     * @param buffer the byte array to update the checksum with
     */
    public void update(byte[] buffer)
    {
        update(buffer, 0, buffer.length);
    }

    public void update(int b)
    {
        update(new byte[]{(byte)b}, 0, 1);
    }

    public int getValue()
    {
        return value & 0xff;
    }

    public void reset()
    {
        value = init;
    }

}
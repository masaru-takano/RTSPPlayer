package com.gclue.android.rtspplayer.core.h264;


public class H264Packet {

    public enum NalType {
        FULL,
        FUA,
        STAPA,
        UNKNOWN
    }

    private final byte nalFBits;
    private final byte nalNriBits;
    private final byte nalType;
    private boolean fuStart = false;
    private boolean fuEnd = false;
    private byte fuNalType;
    public final NalType h264NalType;

    public H264Packet(final byte[] packet) {
        // Parsing the RTP Packet - http://www.ietf.org/rfc/rfc3984.txt section 5.3
        byte nalUnitOctet = packet[0];
        nalFBits = (byte) ((nalUnitOctet & 0x80) >> 15);
        nalNriBits = (byte) (nalUnitOctet & 0x60);
        nalType = (byte) (nalUnitOctet & 0x1F);

        // If it's a single NAL packet then the entire payload is here
        if (nalType > 0 && nalType < 24) {
            h264NalType = NalType.FULL;
        } else if (nalType == 28) {
            h264NalType = NalType.FUA;
        } else if (nalType == 24) {
            h264NalType = NalType.STAPA;
        } else {
            h264NalType = NalType.UNKNOWN;
        }

        byte fuHeader = packet[1];
        fuStart = ((fuHeader & 0x80) != 0);
        fuEnd = ((fuHeader & 0x40) != 0);
        fuNalType = (byte) (fuHeader & 0x1F);
    }

    public byte getNalType() {
        return nalType;
    }

    public byte getNalTypeOctet() {
        return (byte) (fuNalType | nalFBits | nalNriBits);
    }

    public boolean isStartOfFrame() {
        return fuStart && h264NalType == NalType.FUA;
    }

    public boolean isEndOfFrame() {
        return fuEnd && h264NalType == NalType.FUA;
    }

    @Override
    public String toString() {
        return "Type=" + h264NalType + "(=" + nalType + ")" + ", EoF=" + isEndOfFrame();
    }
}

package com.gclue.android.rtspplayer.core.rtp;


public class RTPPacket {
    //size of the RTP header:
    static final int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    private final int Version;
    private final int Padding;
    private final int Extension;
    private final int CC;
    private final int Marker;
    private final int PayloadType;
    private final int SequenceNumber;
    private final int TimeStamp;
    private final int Ssrc;

    //Bitstream of the RTP header
    private byte[] header;

    //size of the RTP payload
    private int mPayloadSize;
    //Bitstream of the RTP payload
    private byte[] mPacket;

    public RTPPacket(final byte[] packet) {
        mPacket = packet;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //check if total packet size is lower than the header size
        int packetSize = packet.length;
        if (packetSize >= HEADER_SIZE) {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++) {
                header[i] = packet[i];
            }

            //get the payload bitstream:
            mPayloadSize = packetSize - HEADER_SIZE;

            //interpret the changing fields of the header:
            Version = (header[0] & 0xFF) >>> 6;
            PayloadType = header[1] & 0x7F;
            SequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            TimeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
        } else {
            Version = 2;
            PayloadType = 0;
            SequenceNumber = 0;
            TimeStamp = 0;
        }
    }

    public byte[] getPayload() {
        byte[] array = new byte[mPayloadSize];
        for (int i = HEADER_SIZE; i < mPacket.length; i++) {
            array[i] = mPacket[i];
        }
        return array;
    }
}

typedef bit<4> PortId;

const PortId REAL_PORT_COUNT = 4w8;

struct InControl {
    PortId inputPort;
}

const PortId RECIRCULATE_IN_PORT = 4w13;

const PortId CPU_IN_PORT = 4w14;

struct OutControl {
    PortId outputPort;
}

const PortId DROP_PORT = 4w15;

const PortId CPU_OUT_PORT = 4w14;

const PortId RECIRCULATE_OUT_PORT = 4w13;

parser Parser<H>(packet_in b, out H parsedHeaders);

control Pipe<H>(inout H headers, in error parseError, in InControl inCtrl, out OutControl outCtrl);

control Deparser<H>(inout H outputHeaders, packet_out b);

package VSS<H>(Parser p, Pipe map, Deparser d);

extern Ck16 {
    Ck16();
    void clear();
    void update<T>(in T data);
    bit<16> get();
}
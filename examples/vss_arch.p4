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

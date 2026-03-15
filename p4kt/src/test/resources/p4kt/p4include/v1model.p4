typedef bit<9> PortId_t;

struct standard_metadata_t {
    PortId_t ingress_port;
    PortId_t egress_spec;
    PortId_t egress_port;
    bit<32> instance_type;
    bit<32> packet_length;
    bit<32> enq_timestamp;
    bit<19> enq_qdepth;
    bit<32> deq_timedelta;
    bit<19> deq_qdepth;
    bit<48> ingress_global_timestamp;
    bit<48> egress_global_timestamp;
    bit<16> mcast_grp;
    bit<16> egress_rid;
    bit<1> checksum_error;
    error parser_error;
    bit<3> priority;
}

extern action_profile {
    action_profile();
}

extern action_selector {
    action_selector();
}
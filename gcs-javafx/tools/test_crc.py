def crc_accumulate(data, crc):
    """MAVLink reference CRC implementation (from mavlink_types.h)"""
    tmp = data ^ (crc & 0xff)
    tmp ^= (tmp << 4) & 0xFF
    result = ((tmp << 8) | ((crc >> 8) & 0xff)) ^ (tmp >> 4) ^ ((tmp << 3) & 0xFFFF)
    return result & 0xFFFF

def crc_calculate(data):
    crc = 0xFFFF
    for byte in data:
        crc = crc_accumulate(byte, crc)
    return crc

# Test with 123456789
test_data = b'123456789'
result = crc_calculate(test_data)
print(f'MAVLink CRC of 123456789: 0x{result:04X}')

# My bit-by-bit version
def crc16_x25(data):
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 1:
                crc = (crc >> 1) ^ 0x8408
            else:
                crc >>= 1
    return crc & 0xFFFF

result2 = crc16_x25(test_data)
print(f'My bit-by-bit CRC: 0x{result2:04X}')
print(f'Match: {result == result2}')

# Now test with a HEARTBEAT frame
# LEN=9, SEQ=0, SYSID=1, COMPID=1, MSGID=0, 9-byte zero payload
header_payload = bytes([9, 0, 1, 1, 0] + [0]*9)
crc_hb = crc_calculate(header_payload)
crc_hb_extra = crc_calculate(header_payload + bytes([50]))
print(f'')
print(f'HEARTBEAT CRC (no extra): 0x{crc_hb:04X}')
print(f'HEARTBEAT CRC (with extra 50): 0x{crc_hb_extra:04X}')
print(f'CK_A: 0x{crc_hb_extra & 0xFF:02X}, CK_B: 0x{(crc_hb_extra >> 8) & 0xFF:02X}')

# Let's also test with SYS_STATUS
# LEN=31, SEQ=0, SYSID=1, COMPID=1, MSGID=1, 31-byte payload
ss_payload = [0]*31
ss_payload[0] = 0xF2  # voltage_battery low
ss_payload[1] = 0x2B  # voltage_battery high
ss_payload[10] = 75   # battery_remaining
ss_data = bytes([31, 0, 1, 1, 1] + ss_payload)
crc_ss = crc_calculate(ss_data + bytes([124]))
print(f'')
print(f'SYS_STATUS CRC (with extra 124): 0x{crc_ss:04X}')

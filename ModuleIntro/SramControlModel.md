
# Entity: SramControlModel 
- **File**: SramControlModel.v

## Diagram
![Diagram](SramControlModel.svg "Diagram")
## Ports

| Port name           | Direction | Type   | Description |
| ------------------- | --------- | ------ | ----------- |
| clock               | input     |        |             |
| reset               | input     |        |             |
| io_SramWrData_valid | input     |        |             |
| io_SramWrData_data  | input     | [7:0]  |             |
| io_SramWrData_ready | output    |        |             |
| io_SramWrData_last  | input     |        |             |
| io_SramWrAddr_valid | input     |        |             |
| io_SramWrAddr_data  | input     | [31:0] |             |
| io_SramWrAddr_ready | output    |        |             |
| io_SramWrAddr_last  | input     |        |             |
| io_SramRdData_valid | output    |        |             |
| io_SramRdData_data  | output    | [7:0]  |             |
| io_SramRdData_ready | input     |        |             |
| io_SramRdData_last  | output    |        |             |
| io_SramRdAddr_valid | input     |        |             |
| io_SramRdAddr_data  | input     | [31:0] |             |
| io_SramRdAddr_ready | output    |        |             |
| io_SramRdAddr_last  | input     |        |             |

## Instantiations

- sramcontrol: SramControl
SramControl 模块
功能：
1.接受MMU发送的写数据请求，根据地址写入数据
2.接受MMU发送的读数据请求，根据地址读取数据
3.将数据写和读的信息转发给MMU模块。
4.控制内部的Sram存储器
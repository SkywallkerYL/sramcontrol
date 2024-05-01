
# Entity: PrioritySelectModel 
- **File**: PrioritySelectModel.v

## Diagram
![Diagram](PrioritySelectModel.svg "Diagram")
## Ports

| Port name       | Direction | Type  | Description |
| --------------- | --------- | ----- | ----------- |
| clock           | input     |       |             |
| reset           | input     |       |             |
| io_FifoEmpty_15 | input     |       |             |
| io_FifoEmpty_14 | input     |       |             |
| io_FifoEmpty_13 | input     |       |             |
| io_FifoEmpty_12 | input     |       |             |
| io_FifoEmpty_11 | input     |       |             |
| io_FifoEmpty_10 | input     |       |             |
| io_FifoEmpty_9  | input     |       |             |
| io_FifoEmpty_8  | input     |       |             |
| io_FifoEmpty_7  | input     |       |             |
| io_FifoEmpty_6  | input     |       |             |
| io_FifoEmpty_5  | input     |       |             |
| io_FifoEmpty_4  | input     |       |             |
| io_FifoEmpty_3  | input     |       |             |
| io_FifoEmpty_2  | input     |       |             |
| io_FifoEmpty_1  | input     |       |             |
| io_FifoEmpty_0  | input     |       |             |
| io_Prior        | output    | [2:0] |             |

## Instantiations

- priorityselect: PrioritySelect


# Entity: PrioritySelectModel 
- **File**: PrioritySelectModel.v

## Diagram
![Diagram](PrioritySelectModel.svg "Diagram")
## Ports

| Port name      | Direction | Type  | Description |
| -------------- | --------- | ----- | ----------- |
| clock          | input     |       |             |
| reset          | input     |       |             |
| io_FifoEmpty_7 | input     |       |             |
| io_FifoEmpty_6 | input     |       |             |
| io_FifoEmpty_5 | input     |       |             |
| io_FifoEmpty_4 | input     |       |             |
| io_FifoEmpty_3 | input     |       |             |
| io_FifoEmpty_2 | input     |       |             |
| io_FifoEmpty_1 | input     |       |             |
| io_FifoEmpty_0 | input     |       |             |
| io_Prior       | output    | [2:0] |             |

## Signals

| Name                       | Type       | Description |
| -------------------------- | ---------- | ----------- |
| priorityselect_FifoEmpty_7 | wire       |             |
| priorityselect_FifoEmpty_6 | wire       |             |
| priorityselect_FifoEmpty_5 | wire       |             |
| priorityselect_FifoEmpty_4 | wire       |             |
| priorityselect_FifoEmpty_3 | wire       |             |
| priorityselect_FifoEmpty_2 | wire       |             |
| priorityselect_FifoEmpty_1 | wire       |             |
| priorityselect_FifoEmpty_0 | wire       |             |
| priorityselect_Prior       | wire [2:0] |             |

## Instantiations

- priorityselect: PrioritySelect

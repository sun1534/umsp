package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.sgip.Constants.Commands;

public class Report extends SgipDataPacket {

	public int submit_node_id;

	public int submit_time_stamp;

	public int submit_sequence_id;

	public byte report_type;

	public String user_number;

	public byte state;

	public byte error_code;

	public String sp_number;

	public Report() {
		super(Commands.REPORT);
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeInt(submit_node_id);
		out.writeInt(submit_time_stamp);
		out.writeInt(submit_sequence_id);

		out.writeByte(report_type);
		writeFixedString(out, user_number, 21);
		out.writeByte(state);
		out.writeByte(error_code);
		writeFixedString(out, reserve, 8);
	}

	public void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		submit_node_id = in.readInt();
		submit_time_stamp = in.readInt();
		submit_sequence_id = in.readInt();

		report_type = in.readByte();
		user_number = readFixedString(in, 21);
		state = in.readByte();
		error_code = in.readByte();
		reserve = readFixedString(in, 8);
	}

	@Override
	public Report clone() {
		return (Report) super.clone();
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 36;
	}

	@Override
	public String toString() {
		return "SGIPReport [command=" + command + ", node_id=" + node_id + ", timestamp=" + timestamp + ", sequence="
				+ sequence + ", submit_node_id=" + submit_node_id + ", submit_time_stamp=" + submit_time_stamp
				+ ", submit_sequence_id=" + submit_sequence_id + ", report_type=" + report_type + ", user_number="
				+ user_number + ", state=" + state + ", error_code=" + error_code + ", reserve=" + reserve
				+ ", sp_number=" + sp_number + "]";
	}

}

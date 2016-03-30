package raft;

import io.netty.channel.ChannelFuture;
import logger.Logger;
import node.timer.NodeTimer;
import raft.proto.AppendEntriesRPC.AppendEntriesPacket;
import raft.proto.AppendEntriesRPC.AppendEntriesResponse;
import raft.proto.AppendEntriesRPC.AppendEntriesResponse.IsUpdated;
import raft.proto.AppendEntriesRPC.LogEntries;
import raft.proto.HeartBeatRPC.HeartBeatPacket;
import raft.proto.HeartBeatRPC.HeartBeatResponse;
import raft.proto.VoteRPC.ResponseVoteRPC;
import raft.proto.VoteRPC.VoteRPCPacket;
import raft.proto.Work.WorkMessage;
import server.ServerUtils;
import server.edges.EdgeInfo;

public class CandidateService extends Service implements Runnable {

	private static CandidateService INSTANCE = null;
	private int numberOfYESResponses;
	private int TotalResponses;
	NodeTimer timer = new NodeTimer();

	private CandidateService() {
		// TODO Auto-generated constructor stub
	}

	public static CandidateService getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CandidateService();
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		Logger.DEBUG("Candidate Service Started");
		startElection();
		while (running) {

		}
	}

	private void startElection() {
		numberOfYESResponses = 0;
		TotalResponses = 0;

		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {

			if (ei.isActive() && ei.getChannel() != null) {
				WorkMessage workMessage = ServiceUtils.prepareRequestVoteRPC();
				Logger.DEBUG("Sent VoteRequestRPC to " + ei.getRef());
				ChannelFuture cf = ei.getChannel().writeAndFlush(workMessage);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send message (VoteRequestRPC) to server");
				}
			}
		}

		timer.schedule(new Runnable() {
			@Override
			public void run() {

				if (isWinner()) {
					NodeState.getInstance().setState(NodeState.LEADER);
				} else {
					NodeState.getInstance().setState(NodeState.FOLLOWER);
				}
			}

			private Boolean isWinner() {

				if ((numberOfYESResponses + 1) > (TotalResponses + 1) / 2) {
					return Boolean.TRUE;
				}
				return Boolean.FALSE;

			}
		}, ServerUtils.getFixedTimeout());

	}

	

	@Override
	public void handleResponseVoteRPCs(WorkMessage workMessage) {
		TotalResponses++;
		if (workMessage.getVoteRPCPacket().getResponseVoteRPC()
				.getIsVoteGranted() == ResponseVoteRPC.IsVoteGranted.YES) {
			numberOfYESResponses++;
		}

	}

	@Override
	public WorkMessage handleRequestVoteRPC(WorkMessage workMessage) {

		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		VoteRPCPacket.Builder voteRPCPacket = VoteRPCPacket.newBuilder();
		voteRPCPacket.setUnixTimestamp(ServerUtils.getCurrentUnixTimeStamp());

		ResponseVoteRPC.Builder responseVoteRPC = ResponseVoteRPC.newBuilder();
		responseVoteRPC.setTerm(1);
		responseVoteRPC.setIsVoteGranted(ResponseVoteRPC.IsVoteGranted.YES);

		voteRPCPacket.setResponseVoteRPC(responseVoteRPC);

		work.setVoteRPCPacket(voteRPCPacket);

		return work.build();

	}

	@Override
	public void handleHeartBeat(WorkMessage wm) {
		Logger.DEBUG("HeartbeatPacket received from leader :" + wm.getHeartBeatPacket().getHeartbeat().getLeaderId());

		NodeState.getInstance().setState(NodeState.FOLLOWER);

	}

	
	

	public void startService(Service service) {
		running = Boolean.TRUE;
		cthread = new Thread((CandidateService) service);
		cthread.start();
	}

	public void stopService() {
		timer.cancel();
		running = Boolean.FALSE;

	}

}
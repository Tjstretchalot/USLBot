package me.timothy.bots.database;

/**
 * A database that contains a mapping for all inmemory models
 * to and from the database of choice.
 * 
 * @author Timothy Moore
 */
public interface MappingDatabase {
	public FullnameMapping getFullnameMapping();
	public MonitoredSubredditMapping getMonitoredSubredditMapping();
	public PersonMapping getPersonMapping();
	public HandledModActionMapping getHandledModActionMapping();
	public BanHistoryMapping getBanHistoryMapping();
	public ResponseMapping getResponseMapping();
	public SubscribedHashtagMapping getSubscribedHashtagMapping();
	public SubredditModqueueProgressMapping getSubredditModqueueProgressMapping();
	public SubredditPropagateStatusMapping getSubredditPropagateStatusMapping();
	public UnbanHistoryMapping getUnbanHistoryMapping();
	public HandledAtTimestampMapping getHandledAtTimestampMapping();
	public TraditionalScammerMapping getTraditionalScammerMapping();
	public UnbanRequestMapping getUnbanRequestMapping();
	public SubredditTraditionalListStatusMapping getSubredditTraditionalListStatusMapping();
	public LastInfoPMMapping getLastInfoPMMapping();
	public SiteSessionMapping getSiteSessionMapping();
	public RegisterAccountRequestMapping getRegisterAccountRequestMapping();
	public ResetPasswordRequestMapping getResetPasswordRequestMapping(); 
	public ActionLogMapping getActionLogMapping();
	public TemporaryAuthLevelMapping getTemporaryAuthLevelMapping();
	public TemporaryAuthRequestMapping getTemporaryAuthRequestMapping();
	public AcceptModeratorInviteRequestMapping getAcceptModeratorInviteRequestMapping();
	public HashtagMapping getHashtagMapping();
	public USLActionMapping getUSLActionMapping();
	public USLActionBanHistoryMapping getUSLActionBanHistoryMapping();
	public USLActionHashtagMapping getUSLActionHashtagMapping();
	public USLActionUnbanHistoryMapping getUSLActionUnbanHistoryMapping();
	public DirtyPersonMapping getDirtyPersonMapping();
	public RedditToMeaningProgressMapping getRedditToMeaningProgressMapping();
	public DeletedPersonMapping getDeletedPersonMapping();
	public PropagatorSettingMapping getPropagatorSettingMapping();
	public MonitoredSubredditAltModMailMapping getMonitoredSubredditAltModMailMapping();
	public RepropagationRequestMapping getRepropagationRequestMapping();
	public SubredditPersonBannedReleaseMapping getSubredditPersonBannedReleaseMapping();
}

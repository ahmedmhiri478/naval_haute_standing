package com.example.cloud

import com.example.model.NavalMachineryType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cloud Sync & Team Collaboration Manager for Marine Engineering Projects.
 */
data class TeamMember(
    val id: String,
    val name: String,
    val role: String,
    val organization: String,
    val avatarInitials: String,
    val isOnline: Boolean
)

data class ReviewComment(
    val id: String,
    val author: TeamMember,
    val timestamp: String,
    val text: String,
    val status: ReviewStatus,
    val partLocation: String = "General Assembly"
)

enum class ReviewStatus(val label: String) {
    APPROVED("Approved"),
    CHANGES_REQUESTED("Changes Requested"),
    UNDER_REVIEW("Under Review")
}

data class RevisionEntry(
    val revisionTag: String,
    val authorName: String,
    val timestamp: String,
    val commitMessage: String,
    val hash: String
)

data class NavalProject(
    val id: String,
    val name: String,
    val vesselType: String,
    val classificationSociety: String,
    val machineryType: NavalMachineryType,
    val lastSyncTime: String,
    val syncState: SyncState = SyncState.SYNCED,
    val activeMembers: List<TeamMember>,
    val comments: List<ReviewComment>,
    val revisions: List<RevisionEntry>
)

enum class SyncState(val label: String) {
    SYNCED("Synced with Cloud"),
    SYNCING("Syncing with Team Cloud..."),
    OFFLINE("Cached Offline")
}

object TeamSyncManager {

    val currentEngineer = TeamMember(
        id = "user_1",
        name = "Capt. Erik Lindqvist",
        role = "Chief Naval Architect",
        organization = "Nordic Marine Engineering",
        avatarInitials = "EL",
        isOnline = true
    )

    val teamRoster = listOf(
        currentEngineer,
        TeamMember(
            id = "user_2",
            name = "Dr. Laura Vance",
            role = "Marine Propulsion Specialist",
            organization = "DNV Maritime",
            avatarInitials = "LV",
            isOnline = true
        ),
        TeamMember(
            id = "user_3",
            name = "Marc Chen",
            role = "Structural FEA Analyst",
            organization = "Naval Hydrodynamics Lab",
            avatarInitials = "MC",
            isOnline = true
        ),
        TeamMember(
            id = "user_4",
            name = "Sven Larsson",
            role = "Lloyd's Senior Marine Surveyor",
            organization = "Lloyd's Register",
            avatarInitials = "SL",
            isOnline = false
        )
    )

    fun getDefaultProjects(): List<NavalProject> {
        val now = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date())

        return listOf(
            NavalProject(
                id = "proj_arctic_prop",
                name = "Arctic Chem-Tanker CPP Propeller",
                vesselType = "16,000 DWT Chemical Tanker (Ice Class 1A)",
                classificationSociety = "DNV-RU-SHIP",
                machineryType = NavalMachineryType.CONTROLLABLE_PITCH_PROPELLER,
                lastSyncTime = "Just now",
                syncState = SyncState.SYNCED,
                activeMembers = listOf(teamRoster[0], teamRoster[1], teamRoster[2]),
                comments = listOf(
                    ReviewComment(
                        id = "c1",
                        author = teamRoster[1],
                        timestamp = "10 mins ago",
                        text = "Blade root fillet section modulus verified for ice collision scenario. Cavitation inception margin acceptable at 15.2 knots.",
                        status = ReviewStatus.APPROVED,
                        partLocation = "Blade Root Fillet (r=0.25R)"
                    ),
                    ReviewComment(
                        id = "c2",
                        author = teamRoster[2],
                        timestamp = "25 mins ago",
                        text = "Von Mises stress peak is 164 MPa against 250 MPa yield for Ni-Al Bronze Cu3. Safety factor 1.52 conforms to DNV rules.",
                        status = ReviewStatus.APPROVED,
                        partLocation = "Trailing Edge Suction Face"
                    )
                ),
                revisions = listOf(
                    RevisionEntry("Rev 2.4", "Capt. Erik Lindqvist", "Today 09:30", "Applied +12mm chord thickness reinforcement at 0.35R", "7f3a9e2"),
                    RevisionEntry("Rev 2.3", "Dr. Laura Vance", "Yesterday 16:45", "Tuned blade rake angle to 9.5° for hull wake clearance", "1b4c89d"),
                    RevisionEntry("Rev 2.0", "Marc Chen", "2 days ago", "Base hydrodynamic geometry generated from Wageningen B-Series", "4e82fa0")
                )
            ),
            NavalProject(
                id = "proj_container_shaft",
                name = "14,000 TEU Shafting & Stern Bearing",
                vesselType = "Ultra Large Container Vessel (ULCV)",
                classificationSociety = "Lloyd's Register",
                machineryType = NavalMachineryType.PROPULSION_SHAFTING,
                lastSyncTime = "3 hours ago",
                syncState = SyncState.SYNCED,
                activeMembers = listOf(teamRoster[0], teamRoster[3]),
                comments = listOf(
                    ReviewComment(
                        id = "c3",
                        author = teamRoster[3],
                        timestamp = "1 hour ago",
                        text = "Stern tube bearing white-metal reaction angle aligned with propeller overhang bending curve.",
                        status = ReviewStatus.APPROVED,
                        partLocation = "Aft Stern Tube Bearing Journal"
                    )
                ),
                revisions = listOf(
                    RevisionEntry("Rev 1.8", "Capt. Erik Lindqvist", "Yesterday", "Reduced inner hollow bore diameter to 140mm for torque margin", "8a2f1c4"),
                    RevisionEntry("Rev 1.0", "Capt. Erik Lindqvist", "3 days ago", "Initial shaft line schematic import", "9c12b77")
                )
            ),
            NavalProject(
                id = "proj_lng_head",
                name = "Dual-Fuel Diesel Cylinder Head",
                vesselType = "174,000 m3 LNG Carrier Auxiliary Genset",
                classificationSociety = "Bureau Veritas",
                machineryType = NavalMachineryType.DIESEL_CYLINDER_HEAD,
                lastSyncTime = "Yesterday",
                syncState = SyncState.SYNCED,
                activeMembers = listOf(teamRoster[0], teamRoster[2]),
                comments = listOf(
                    ReviewComment(
                        id = "c4",
                        author = teamRoster[2],
                        timestamp = "Yesterday",
                        text = "Cooling water jacket channel geometry optimized to reduce exhaust valve bridge thermal gradient.",
                        status = ReviewStatus.UNDER_REVIEW,
                        partLocation = "Exhaust Valve Bridge"
                    )
                ),
                revisions = listOf(
                    RevisionEntry("Rev 1.2", "Marc Chen", "Yesterday", "FEA thermal-mechanical coupling simulation updated", "33d8fa1")
                )
            )
        )
    }
}

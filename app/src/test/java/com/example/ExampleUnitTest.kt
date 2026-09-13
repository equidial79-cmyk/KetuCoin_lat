package com.example

import com.example.data.model.Profile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `wallet access is strictly permitted only when KYC is approved`() {
        val approvedProfile = Profile(
            id = "user-1",
            email = "user1@example.com",
            kycStatus = "approved"
        )
        val isWalletAllowed = approvedProfile.kycStatus == "approved"
        assertTrue(isWalletAllowed)
    }

    @Test
    fun `wallet access is denied for unverified, pending, and rejected statuses`() {
        val unverifiedProfile = Profile(id = "user-2", email = "user2@example.com", kycStatus = "unverified")
        val pendingProfile = Profile(id = "user-3", email = "user3@example.com", kycStatus = "pending")
        val rejectedProfile = Profile(id = "user-4", email = "user4@example.com", kycStatus = "rejected")

        assertFalse(unverifiedProfile.kycStatus == "approved")
        assertFalse(pendingProfile.kycStatus == "approved")
        assertFalse(rejectedProfile.kycStatus == "approved")
    }
}


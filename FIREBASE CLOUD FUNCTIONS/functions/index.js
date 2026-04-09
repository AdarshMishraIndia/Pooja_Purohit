const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { auth } = require("firebase-functions/v2"); // Use the top-level auth object
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// --- CONSTANTS & HELPERS ---
const COLLECTION_NOTIFICATIONS = "notifications";
const SUBCOLLECTION_ITEMS = "items";

const createNotification = async (uid, { title, body, type, deepLink }) => {
    if (!uid) return null;
    const ref = db.collection(COLLECTION_NOTIFICATIONS).doc(uid).collection(SUBCOLLECTION_ITEMS).doc();
    return ref.set({
        title,
        body,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false,
        type: type || "GENERAL",
        deepLinkUrl: deepLink || null
    });
};

// --- TRIGGER HANDLERS ---

/**
 * 1. Welcome Notification
 * Updated to use auth.onUserCreated for v2 compliance
 */
exports.handleWelcomeNotification = auth.onUserCreated(async (event) => {
    const user = event.data;
    return createNotification(user.uid, {
        title: "Welcome to Pooja Purohit! 🙏",
        body: "Your spiritual journey starts here. Explore our services and book your first ritual today.",
        type: "GENERAL",
        deepLink: "poojapurohit://home"
    });
});

/**
 * 2. Booking Status Notifications
 */
exports.handleBookingUpdate = onDocumentUpdated("bookings/{bookingId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();
    const bookingId = event.params.bookingId;

    if (!newData || !oldData || newData.status === oldData.status) return null;

    const tasks = [];

    if (newData.status === "ACCEPTED") {
        tasks.push(createNotification(newData.userId, {
            title: "Booking Confirmed!",
            body: `Your ${newData.serviceName} is confirmed for ${newData.scheduledDate}.`,
            type: "ORDER_UPDATE",
            deepLink: `poojapurohit://booking/${bookingId}`
        }));
    }

    if (newData.status === "CANCELLED") {
        const targetUid = newData.cancelledBy === newData.userId ? newData.purohitId : newData.userId;
        tasks.push(createNotification(targetUid, {
            title: "Booking Cancelled",
            body: `The booking for ${newData.serviceName} has been cancelled.`,
            type: "ALERT",
            deepLink: `poojapurohit://booking/${bookingId}`
        }));
    }

    return Promise.all(tasks);
});

/**
 * 3. Purohit Verification Status
 */
exports.handlePurohitVerification = onDocumentUpdated("purohits/{purohitId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();

    if (newData && oldData && !oldData.isVerified && newData.isVerified) {
        return createNotification(event.params.purohitId, {
            title: "Account Verified! ✅",
            body: "Your profile is verified. You are now visible to users and can accept bookings.",
            type: "ALERT",
            deepLink: "poojapurohit://profile"
        });
    }
    return null;
});

/**
 * 4. Scheduled Reminders for Purohits
 */
exports.purohitScheduledReminders = onSchedule("0 * * * *", async (event) => {
    const now = Date.now();
    const milestones = [
        { label: "48h", ms: 48 * 60 * 60 * 1000 },
        { label: "24h", ms: 24 * 60 * 60 * 1000 },
        { label: "6h", ms: 6 * 60 * 60 * 1000 },
        { label: "1h", ms: 1 * 60 * 60 * 1000 }
    ];

    const snapshot = await db.collection("bookings")
        .where("status", "==", "ACCEPTED")
        .get();

    const batch = db.batch();

    snapshot.forEach(doc => {
        const data = doc.data();
        if (!data.scheduledDate || !data.purohitId) return;

        const eventTime = new Date(data.scheduledDate).getTime();
        const diff = eventTime - now;
        const sentReminders = data.remindersSent || [];

        for (const milestone of milestones) {
            if (diff > 0 && diff <= milestone.ms && !sentReminders.includes(milestone.label)) {
                const notifRef = db.collection(COLLECTION_NOTIFICATIONS)
                    .doc(data.purohitId)
                    .collection(SUBCOLLECTION_ITEMS)
                    .doc();

                batch.set(notifRef, {
                    title: "Upcoming Service Reminder",
                    body: `Reminder: You have a ${data.serviceName} in ${milestone.label}.`,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    isRead: false,
                    type: "GENERAL",
                    deepLinkUrl: `poojapurohit://booking/${doc.id}`
                });

                batch.update(doc.ref, {
                    remindersSent: admin.firestore.FieldValue.arrayUnion(milestone.label)
                });
                break; 
            }
        }
    });

    return batch.commit();
});
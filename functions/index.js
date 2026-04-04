const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onChatMessageAdded = onDocumentCreated(
    "messages/{chatId}/history/{messageId}",
    async (event) => {
      const snapshot = event.data;
      const context = event.params;

      if (!snapshot) return;

      const messageData = snapshot.data();
      const chatId = context.chatId;
      const recipientId = messageData.receiverId;
      const senderId = messageData.senderId;
      const messageText = messageData.message;

      const recipientDoc = await admin
          .firestore()
          .collection("users")
          .doc(recipientId)
          .get();

      if (!recipientDoc.exists) {
        console.log("Recipient not found");
        return;
      }

      const fcmToken = recipientDoc.data().fcmToken;

      if (!fcmToken) {
        console.log("No FCM token");
        return;
      }

      const senderDoc = await admin
          .firestore()
          .collection("users")
          .doc(senderId)
          .get();

      const senderName = senderDoc.exists ?
      senderDoc.data().name :
      "New Message";

      const payload = {
        token: fcmToken,
        notification: {
          title: `Message from ${senderName}`,
          body: messageText,
        },
        data: {
          chatId: chatId,
          otherUserId: senderId,
        },
      };

      await admin.messaging().send(payload);
    },
);

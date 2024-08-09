//package app.firebase
//
//import android.net.Uri
//import app.entities.PDF
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.storage.StorageReference
//
//object pdfFB {
//    private val firestore = FirebaseFirestore.getInstance()
//    fun uploadFileToFirebase(
//        fileUri: Uri?,
//        onSuccess: () -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        fileUri?.let {
//            val storageReference: StorageReference = FirebaseStorage.getInstance().reference
//            val fileReference: StorageReference = storageReference.child("uploads/${System.currentTimeMillis()}.pdf")
//
//            fileReference.putFile(fileUri)
//                .addOnSuccessListener { taskSnapshot ->
//                    fileReference.downloadUrl.addOnSuccessListener { uri ->
//                        val pdf = PDF(
//                            id = taskSnapshot.metadata?.name ?: "",
//                            name = fileUri.lastPathSegment ?: "",
//                            url = uri.toString()
//                        )
//                        savePdfToFirestore(pdf, onSuccess, onFailure)
//                    }
//                    onSuccess.invoke()
//                }
//                .addOnFailureListener { e ->
//                    onFailure.invoke("File Upload Failed: ${e.message}")
//                }
//        }
//    }
//
//    private fun savePdfToFirestore(
//        pdf: PDF,
//        onSuccess: () -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        firestore.collection("pdfs")
//            .add(pdf)
//            .addOnSuccessListener {
//                onSuccess.invoke()
//            }
//            .addOnFailureListener { e ->
//                onFailure.invoke("Error saving PDF: ${e.message}")
//            }
//    }
//}
//

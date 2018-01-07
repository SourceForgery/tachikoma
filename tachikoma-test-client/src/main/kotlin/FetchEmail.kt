import com.google.protobuf.Empty
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import io.grpc.ManagedChannelBuilder

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
            .usePlaintext(true)
            .build()

    val stub = MTAEmailQueueGrpc.newBlockingStub(channel)

    try {
        stub.getEmails(Empty.getDefaultInstance()).forEach {
            System.err.println("Got email: " + JsonFormat.printer().print(it))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}
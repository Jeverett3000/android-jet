#include <fstream>
#include <SkPictureRecorder.h>
#include <SkRRect.h>
#include <SkCanvas.h>
#include <SkPaint.h>
#include <SkRect.h>

/**
 * Tool to generate boxes.skp used in testing the layout inspector.
 * Build and run through CLion--it's just for generating test data, and so needn't be built by bazel.
 */
 //TODO: Make this file build the boxes.skp during the run of the test.
int main() {
    SkPictureRecorder recorder;
    SkPaint paint;
    paint.setStyle(SkPaint::kFill_Style);
    paint.setAntiAlias(true);
    paint.setStrokeWidth(0);

    SkCanvas* canvas = recorder.beginRecording({0, 0, 1000, 2000});
    const SkRect &skRect1 = SkRect::MakeXYWH(0, 0, 1000, 2000);
    canvas->drawAnnotation(skRect1, "RenderNode(id=1, name='LinearLayout')", nullptr);
    paint.setColor(SK_ColorYELLOW);
    canvas->drawRect(skRect1, paint);

    const SkRect &skRect2 = SkRect::MakeXYWH(0, 0, 500, 1000);
    canvas->drawAnnotation(skRect2, "RenderNode(id=2, name='FrameLayout')", nullptr);
    canvas->save();
    canvas->translate(100, 100);
    paint.setColor(SK_ColorBLUE);
    canvas->drawRect(skRect2, paint);

    const SkRect &skRect3 = SkRect::MakeXYWH(0, 0, 200, 500);
    canvas->drawAnnotation(skRect3, "RenderNode(id=3, name='AppCompatButton')", nullptr);
    canvas->save();
    canvas->translate(200, 200);
    paint.setColor(SK_ColorBLACK);
    canvas->drawRect(skRect3, paint);
    canvas->restore();
    canvas->drawAnnotation(skRect3, "/RenderNode(id=3, name='AppCompatButton')", nullptr);

    canvas->restore();
    canvas->drawAnnotation(skRect2, "/RenderNode(id=2, name='FrameLayout')", nullptr);

    const SkRect &skRect4 = SkRect::MakeXYWH(0, 0, 400, 500);
    canvas->drawAnnotation(skRect4, "RenderNode(id=4, name='Button')", nullptr);
    canvas->save();
    canvas->translate(300, 1200);
    paint.setColor(SK_ColorRED);
    canvas->drawRect(skRect4, paint);
    canvas->restore();
    canvas->drawAnnotation(skRect4, "/RenderNode(id=4, name='Button')", nullptr);

    canvas->drawAnnotation(skRect1, "/RenderNode(id=1, name='LinearLayout')", nullptr);

    sk_sp<SkPicture> picture = recorder.finishRecordingAsPicture();
    sk_sp<SkData> data = picture->serialize();
    std::ofstream f("boxes.skp", std::ofstream::out);
    f.write(static_cast<const char *>(data->data()), data->size());
    f.close();
}

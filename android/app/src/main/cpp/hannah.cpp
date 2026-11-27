#include <jni.h>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
#include "llama.h"

static std::string bytes(JNIEnv *env, jbyteArray data) {
    std::string out(env->GetArrayLength(data), '\0');
    env->GetByteArrayRegion(data, 0, out.size(), reinterpret_cast<jbyte *>(out.data()));
    return out;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_cyberassistant_ui_ChatClient_generate(JNIEnv *env, jobject, jbyteArray path, jbyteArray input) {
    try {
        static const bool initialized = [] { llama_backend_init(); return true; }();
        (void) initialized;
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        std::unique_ptr<llama_model, decltype(&llama_model_free)> model(
            llama_model_load_from_file(bytes(env, path).c_str(), mp), llama_model_free);
        if (!model) throw std::runtime_error("No se pudo cargar el modelo local.");
        auto vocab = llama_model_get_vocab(model.get());
        auto prompt = bytes(env, input);
        int count = -llama_tokenize(vocab, prompt.data(), prompt.size(), nullptr, 0, false, true);
        if (count < 1 || count > 1792) throw std::runtime_error("El mensaje es demasiado largo para este modelo.");
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, prompt.data(), prompt.size(), tokens.data(), count, false, true) != count)
            throw std::runtime_error("No se pudo procesar el mensaje.");
        auto cp = llama_context_default_params();
        cp.n_ctx = 2048;
        cp.n_batch = 512;
        cp.n_threads = cp.n_threads_batch = 4;
        std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(model.get(), cp), llama_free);
        if (!ctx) throw std::runtime_error("Memoria insuficiente para el modelo.");
        for (int begin = 0; begin < count; begin += 512) {
            if (llama_decode(ctx.get(), llama_batch_get_one(tokens.data()+begin, std::min(512, count-begin))))
                throw std::runtime_error("Error al procesar el contexto.");
        }
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(llama_sampler_init_greedy(), llama_sampler_free);
        std::string result;
        for (int i = 0; i < 160; ++i) {
            auto token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            std::vector<char> part(64);
            int n = llama_token_to_piece(vocab, token, part.data(), part.size(), 0, false);
            if (n < 0) {
                part.resize(-n);
                n = llama_token_to_piece(vocab, token, part.data(), part.size(), 0, false);
            }
            if (n < 0) throw std::runtime_error("Error al leer la respuesta.");
            result.append(part.data(), n);
            if (llama_decode(ctx.get(), llama_batch_get_one(&token, 1)))
                throw std::runtime_error("Error durante la respuesta.");
        }
        auto out = env->NewByteArray(result.size());
        if (out) env->SetByteArrayRegion(out, 0, result.size(), reinterpret_cast<const jbyte *>(result.data()));
        return out;
    } catch (const std::exception &e) {
        env->ThrowNew(env->FindClass("java/io/IOException"), e.what());
        return nullptr;
    }
}

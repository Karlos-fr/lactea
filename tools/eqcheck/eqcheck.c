/*
 * eqcheck: compiles preset equations with projectM's expression evaluator (projectm-eval)
 * and reports which ones fail, with the error position. Runs on the TV (adb shell).
 *
 * Input (stdin), one record per code block:
 *     <id>\t<length>\n<length bytes of code>\n
 * Output (stdout), one line per record:
 *     <id>\tOK
 *     <id>\tERR\t<line>:<column>\t<message>
 */
#include <projectm-eval.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void projectm_eval_memory_host_lock_mutex(void) {}
void projectm_eval_memory_host_unlock_mutex(void) {}

int main(void)
{
    char header[4096];
    while (fgets(header, sizeof(header), stdin))
    {
        char* tab = strrchr(header, '\t');
        if (!tab)
        {
            continue;
        }
        *tab = 0;
        long length = strtol(tab + 1, NULL, 10);
        char* code = malloc((size_t) length + 1);
        if (fread(code, 1, (size_t) length, stdin) != (size_t) length)
        {
            free(code);
            break;
        }
        code[length] = 0;
        fgetc(stdin); /* record separator */

        struct projectm_eval_context* ctx = projectm_eval_context_create(NULL, NULL);
        struct projectm_eval_code* compiled = projectm_eval_code_compile(ctx, code);
        if (compiled)
        {
            printf("%s\tOK\n", header);
            projectm_eval_code_destroy(compiled);
        }
        else
        {
            int line = 0;
            int column = 0;
            const char* error = projectm_eval_get_error(ctx, &line, &column);
            printf("%s\tERR\t%d:%d\t%s\n", header, line, column, error ? error : "?");
        }
        projectm_eval_context_destroy(ctx);
        free(code);
    }
    return 0;
}

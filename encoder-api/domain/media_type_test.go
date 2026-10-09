package domain_test

import (
	"errors"
	"testing"

	"github.com/tarcisiolzbraga/codeflix/encoder/domain"
)

func Test_givenAKnownType_whenParseMediaType_thenReturnIt(t *testing.T) {
	for _, value := range []string{"VIDEO", "TRAILER"} {
		actual, err := domain.ParseMediaType(value)

		if err != nil {
			t.Fatalf("%q devia ser aceito, veio %v", value, err)
		}
		if actual.String() != value {
			t.Errorf("esperava %q, veio %q", value, actual)
		}
	}
}

// IMAGE existe no admin, mas não é convertida — ele nem publica aviso para ela. Se chegar aqui,
// é mensagem que não devia ter saído, e recusar é mais honesto que converter errado.
func Test_givenATypeWeDoNotConvert_whenParseMediaType_thenFail(t *testing.T) {
	for _, value := range []string{"IMAGE", "BANNER", "video", ""} {
		_, err := domain.ParseMediaType(value)

		if !errors.Is(err, domain.ErrUnknownMediaType) {
			t.Errorf("%q devia ser recusado, veio %v", value, err)
		}
	}
}

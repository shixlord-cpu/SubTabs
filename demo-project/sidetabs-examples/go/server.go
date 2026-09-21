package catalog

import (
	"fmt"
	"net/http"
)

type Product struct {
	Name  string
	Price float64
}

type Server struct {
	products []Product
}

func NewServer() *Server {
	return &Server{
		products: []Product{{Name: "Mug", Price: 12}},
	}
}

func (server *Server) ServeHTTP(writer http.ResponseWriter, request *http.Request) {
	fmt.Fprintf(writer, "%d products at %s", len(server.products), request.URL.Path)
}

func main() {
	http.ListenAndServe(":8080", NewServer())
}
